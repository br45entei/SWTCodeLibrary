package org.bouncycastle.math.ec.custom.sec;

import java.math.BigInteger;
import java.util.Random;

import org.bouncycastle.math.ec.ECConstants;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECCurve.AbstractF2m;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

public class SecT113R1Curve extends AbstractF2m
{
    private static final int SecT113R1_DEFAULT_COORDS = COORD_LAMBDA_PROJECTIVE;

    protected SecT113R1Point infinity;

    public SecT113R1Curve()
    {
        super(113, 9, 0, 0);

        this.infinity = new SecT113R1Point(this, null, null);

        this.a = fromBigInteger(new BigInteger(1, Hex.decode("003088250CA6E7C7FE649CE85820F7")));
        this.b = fromBigInteger(new BigInteger(1, Hex.decode("00E8BEE4D3E2260744188BE0E9C723")));
        this.order = new BigInteger(1, Hex.decode("0100000000000000D9CCEC8A39E56F"));
        this.cofactor = BigInteger.valueOf(2);

        this.coord = SecT113R1_DEFAULT_COORDS;
    }

    protected ECCurve cloneCurve()
    {
        return new SecT113R1Curve();
    }

    public boolean supportsCoordinateSystem(int coord)
    {
        switch (coord)
        {
        case COORD_LAMBDA_PROJECTIVE:
            return true;
        default:
            return false;
        }
    }

    public int getFieldSize()
    {
        return 113;
    }

    public ECFieldElement fromBigInteger(BigInteger x)
    {
        return new SecT113FieldElement(x);
    }

    protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, boolean withCompression)
    {
        return new SecT113R1Point(this, x, y, withCompression);
    }

    protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, ECFieldElement[] zs, boolean withCompression)
    {
        return new SecT113R1Point(this, x, y, zs, withCompression);
    }

    public ECPoint getInfinity()
    {
        return infinity;
    }

    public boolean isKoblitz()
    {
        return false;
    }

    /**
     * Decompresses a compressed point P = (xp, yp) (X9.62 s 4.2.2).
     *
     * @param yTilde
     *            ~yp, an indication bit for the decompression of yp.
     * @param X1
     *            The field element xp.
     * @return the decompressed point.
     */
    protected ECPoint decompressPoint(int yTilde, BigInteger X1)
    {
        ECFieldElement x = fromBigInteger(X1), y = null;
        if (x.isZero())
        {
            y = b.sqrt();
        }
        else
        {
            ECFieldElement beta = x.square().invert().multiply(b).add(a).add(x);
            ECFieldElement z = solveQuadraticEquation(beta);
            if (z != null)
            {
                if (z.testBitZero() != (yTilde == 1))
                {
                    z = z.addOne();
                }

                switch (this.getCoordinateSystem())
                {
                case COORD_LAMBDA_AFFINE:
                case COORD_LAMBDA_PROJECTIVE:
                {
                    y = z.add(x);
                    break;
                }
                default:
                {
                    y = z.multiply(x);
                    break;
                }
                }
            }
        }

        if (y == null)
        {
            throw new IllegalArgumentException("Invalid point compression");
        }

        return this.createRawPoint(x, y, true);
    }

    /**
     * Solves a quadratic equation <code>z<sup>2</sup> + z = beta</code>(X9.62
     * D.1.6) The other solution is <code>z + 1</code>.
     *
     * @param beta
     *            The value to solve the quadratic equation for.
     * @return the solution for <code>z<sup>2</sup> + z = beta</code> or
     *         <code>null</code> if no solution exists.
     */
    private ECFieldElement solveQuadraticEquation(ECFieldElement beta)
    {
        if (beta.isZero())
        {
            return beta;
        }

        ECFieldElement zeroElement = fromBigInteger(ECConstants.ZERO);

        ECFieldElement z = null;
        ECFieldElement gamma = null;

        Random rand = new Random();
        do
        {
            ECFieldElement t = fromBigInteger(new BigInteger(113, rand));
            z = zeroElement;
            ECFieldElement w = beta;
            for (int i = 1; i < 113; i++)
            {
                ECFieldElement w2 = w.square();
                z = z.square().add(w2.multiply(t));
                w = w2.add(beta);
            }
            if (!w.isZero())
            {
                return null;
            }
            gamma = z.square().add(z);
        }
        while (gamma.isZero());

        return z;
    }

    public int getM()
    {
        return 113;
    }

    public boolean isTrinomial()
    {
        return true;
    }

    public int getK1()
    {
        return 9;
    }

    public int getK2()
    {
        return 0;
    }

    public int getK3()
    {
        return 0;
    }
}
